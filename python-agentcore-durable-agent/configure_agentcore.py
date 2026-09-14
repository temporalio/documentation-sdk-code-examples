import json
import os
import subprocess
from pathlib import Path
from typing import Any

PROJECT_DIR = Path(__file__).parent
AGENTCORE_CONFIG = PROJECT_DIR / "agentcore" / "agentcore.json"
AWS_TARGETS = PROJECT_DIR / "agentcore" / "aws-targets.json"


def required_environment(name: str) -> str:
    value = os.environ.get(name)
    if not value:
        raise SystemExit(f"Set {name} before running this command.")
    return value


def write_json(path: Path, value: Any) -> None:
    path.write_text(f"{json.dumps(value, indent=2)}\n")


def main() -> None:
    temporal_address = required_environment("TEMPORAL_ADDRESS")
    temporal_namespace = required_environment("TEMPORAL_NAMESPACE")
    temporal_api_key = required_environment("TEMPORAL_API_KEY")
    aws_region = required_environment("AWS_REGION")

    aws_account_id = subprocess.check_output(
        ["aws", "sts", "get-caller-identity", "--query", "Account", "--output", "text"],
        text=True,
    ).strip()

    aws_targets = json.loads(AWS_TARGETS.read_text())
    aws_targets[0]["account"] = aws_account_id
    aws_targets[0]["region"] = aws_region
    write_json(AWS_TARGETS, aws_targets)

    agentcore_config = json.loads(AGENTCORE_CONFIG.read_text())
    environment = {
        item["name"]: item for item in agentcore_config["runtimes"][0]["envVars"]
    }
    environment["TEMPORAL_ADDRESS"]["value"] = temporal_address
    environment["TEMPORAL_NAMESPACE"]["value"] = temporal_namespace
    environment["TEMPORAL_API_KEY"]["value"] = temporal_api_key
    environment["AWS_REGION"]["value"] = aws_region
    write_json(AGENTCORE_CONFIG, agentcore_config)

    print(f"Configured AgentCore for AWS account {aws_account_id} in {aws_region}.")


if __name__ == "__main__":
    main()
