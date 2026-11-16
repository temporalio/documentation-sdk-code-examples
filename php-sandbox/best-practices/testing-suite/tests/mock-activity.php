<?php
declare(strict_types=1);

// Sets or clears an Activity mock in the running RoadRunner server's KV
// storage, the way the PHPUnit test case does. Used by test.sh.
//   RR_RPC=tcp://127.0.0.1:6099 php mock-activity.php complete <value>
//   RR_RPC=tcp://127.0.0.1:6099 php mock-activity.php fail <message>
//   RR_RPC=tcp://127.0.0.1:6099 php mock-activity.php clear

use Temporal\Testing\ActivityMocker;

require __DIR__ . '/../../../vendor/autoload.php';

$activityMocks = new ActivityMocker();
match ($argv[1]) {
    'complete' => $activityMocks->expectCompletion('SimpleActivity.doSomething', $argv[2]),
    'fail' => $activityMocks->expectFailure('SimpleActivity.doSomething', new \LogicException($argv[2])),
    'clear' => $activityMocks->clear(),
};
