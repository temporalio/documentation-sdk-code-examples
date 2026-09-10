#!/bin/bash
set -euo pipefail

SAMPLE_DIR="$(cd "$(dirname "$0")" && pwd)"
CDK_DIR="$SAMPLE_DIR/agentcore/cdk"

if [ -d "$CDK_DIR" ]; then
  echo "AgentCore CDK project already exists at $CDK_DIR"
  exit 0
fi

for tool in agentcore uv; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "'$tool' is required but is not installed." >&2
    exit 1
  fi
done

TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT

agentcore create \
  --project-name TemporalDurableAgent \
  --no-agent \
  --output-dir "$TEMP_DIR" \
  --skip-git \
  --skip-python-setup

mv "$TEMP_DIR/TemporalDurableAgent/agentcore/cdk" "$CDK_DIR"
echo "Created AgentCore CDK project at $CDK_DIR"
