# Durable Strands agent on Amazon Bedrock AgentCore

This sample supports the Temporal guide
[Build a durable agent on Amazon Bedrock AgentCore](https://docs.temporal.io/guides/durable-agent-on-agentcore).

The application keeps one Strands conversation in a long-running Temporal Workflow. Model and AgentCore Code
Interpreter calls run as Activities. The local Worker runs continuously. The AgentCore Runtime handler runs the same
Workflow and Activity code on serverless Worker compute and retires after an idle period.

The Worker configures Strands to use Amazon Nova Lite through Amazon Bedrock. The explicit model selection avoids
depending on Strands' default model, which can change between releases.

When AgentCore invokes the Runtime, the handler starts the Worker in a background task and immediately returns an
acknowledgment. AgentCore reports the Runtime as busy while the Worker runs and clears that status after the Worker
drains or fails. A second invocation in the same Runtime session does not start another Worker.

## Run locally

Install dependencies:

```bash
uv sync
```

Start a local Temporal development server:

```bash
temporal server start-dev
```

In another terminal, start the Worker with AWS credentials that can call Bedrock and AgentCore Code Interpreter:

```bash
uv run python local_worker.py
```

In a third terminal, start a conversation:

```bash
uv run python chat.py
```

Enter `/finish` to close the Workflow.

## Test

The test uses a scripted model and does not call AWS:

```bash
uv run pytest
```

It processes the first prompt on one Worker, stops that Worker, processes a follow-up on a new Worker, and verifies that
the second model call receives the first turn's messages.

## Deploy

Install the AgentCore CLI, then generate its CDK project:

```bash
npm install -g @aws/agentcore
./bootstrap-agentcore-project.sh
```

Populate `agentcore/aws-targets.json` and `agentcore/agentcore.json` from environment variables:

```bash
export TEMPORAL_ADDRESS="<namespace>.<account>.tmprl.cloud:7233"
export TEMPORAL_NAMESPACE="<namespace>.<account>"
printf "Temporal Cloud API key: "
read -rs TEMPORAL_API_KEY
printf "\n"
export TEMPORAL_API_KEY
export AWS_REGION="us-west-2"
uv run python configure_agentcore.py
```

Do not commit a populated Temporal Cloud API key. Then follow the
[AgentCore Serverless Worker deployment guide](https://docs.temporal.io/production-deployment/worker-deployments/serverless-workers/agentcore)
to deploy the Runtime, create its invocation role, and configure the Worker Deployment Version.
