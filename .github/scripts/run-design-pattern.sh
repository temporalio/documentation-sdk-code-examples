#!/usr/bin/env bash
# Runs one design-pattern sample end to end from its runner.json manifest:
# starts a fresh Temporal dev server, runs the manifest's setup, Worker, and
# starter commands, then checks the Workflow's final status and output.
#
# CI and the docs.temporal.io live runner both execute samples from these
# manifests, so a manifest that passes here is what visitors run.
#
# Usage: run-design-pattern.sh path/to/runner.json
# Requires: temporal CLI, jq, and the language toolchain for the sample.
# Set SKIP_SETUP=1 when dependencies are already installed, as they are in the
# live runner's prebuilt sandbox snapshot.

set -euo pipefail

manifest="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
repo_root="$(git -C "$(dirname "$manifest")" rev-parse --show-toplevel)"
field() { jq -r "$1" "$manifest"; }

name="$(field '.pattern')/$(field '.language')"
workdir="$repo_root/$(field '.workdir')"
expect_status="$(field '.expect.status')"
port="${TEMPORAL_PORT:-7233}"
logdir="$(mktemp -d)"

pids=()
cleanup() {
  for pid in "${pids[@]}"; do kill "$pid" 2>/dev/null || true; done
  wait 2>/dev/null || true
}
trap cleanup EXIT

# `timeout` isn't on macOS by default; run without a limit there.
with_timeout() {
  if command -v timeout >/dev/null; then timeout "$@"; else shift; "$@"; fi
}

# Prints the Worker and starter output for debugging, then the failure reason
# last so it's what readers see. Samples that fail on purpose (like Saga) put
# expected stack traces in this output.
fail() {
  for f in worker starter; do
    echo "----- $f output -----"
    cat "$logdir/$f.log" 2>/dev/null || true
  done
  echo "----- end of output -----"
  echo "FAIL [$name]: $*"
  if [ -n "${GITHUB_ACTIONS:-}" ]; then
    echo "::error title=Design pattern $name failed::$*"
  fi
  exit 1
}

echo "== $name: starting Temporal dev server"
temporal server start-dev --headless --port "$port" --log-level error >"$logdir/server.log" 2>&1 &
pids+=($!)
for _ in $(seq 1 60); do
  temporal operator cluster health --address "localhost:$port" >/dev/null 2>&1 && break
  sleep 1
done
temporal operator cluster health --address "localhost:$port" >/dev/null 2>&1 || fail "Temporal dev server didn't become healthy"

cd "$workdir"
if [ "${SKIP_SETUP:-}" != "1" ]; then
  echo "== $name: setup"
  bash -c "$(field '.setup')" || fail "setup command failed"
fi

echo "== $name: starting Worker"
bash -c "$(field '.worker')" >"$logdir/worker.log" 2>&1 &
pids+=($!)

echo "== $name: running starter"
with_timeout 180 bash -c "$(field '.starter')" >"$logdir/starter.log" 2>&1 || fail "starter exited non-zero"

# Each run uses a fresh dev server, so the only Workflow is this sample's.
workflow_id="$(temporal workflow list --address "localhost:$port" -o json --limit 1 | jq -r '.[0].execution.workflowId')"
[ -n "$workflow_id" ] && [ "$workflow_id" != "null" ] || fail "no Workflow Execution found"

status=""
for _ in $(seq 1 60); do
  status="$(temporal workflow describe --address "localhost:$port" -w "$workflow_id" -o json \
    | jq -r '.workflowExecutionInfo.status' | sed 's/^WORKFLOW_EXECUTION_STATUS_//')"
  [ "$status" != "RUNNING" ] && break
  sleep 1
done
echo "== $name: Workflow $workflow_id finished with status $status"
[ "$status" = "$expect_status" ] || fail "expected status $expect_status, got $status"

# Expected output lines must appear in order across the Worker and starter output.
combined="$logdir/combined.log"
cat "$logdir/worker.log" "$logdir/starter.log" >"$combined"
last_line=0
while IFS= read -r expected; do
  [ -z "$expected" ] && continue
  line="$(tail -n "+$((last_line + 1))" "$combined" | grep -n -F -m1 -- "$expected" | cut -d: -f1 || true)"
  [ -n "$line" ] || fail "expected output not found (in order): $expected"
  last_line=$((last_line + line))
done < <(jq -r '.expect.output // [] | .[]' "$manifest")

echo "PASS [$name]"
