<?php
declare(strict_types=1);

// Replays a JSON Event History against the Workflow code that the running
// RoadRunner server has loaded. Prints "ok" or the replay error.
//   RR_RPC=tcp://127.0.0.1:6099 php replay.php <history.json>

use Temporal\Testing\Replay\Exception\ReplayerException;
use Temporal\Testing\Replay\WorkflowReplayer;

require __DIR__ . '/../../vendor/autoload.php';

try {
    (new WorkflowReplayer())->replayFromJSON('MyWorkflow', $argv[1]);
    echo "ok\n";
} catch (ReplayerException $e) {
    echo 'failed: ', $e->getMessage(), "\n";
}
