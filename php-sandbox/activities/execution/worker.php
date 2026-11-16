<?php
declare(strict_types=1);

// Runs the Workflows from the Activity Execution page on the
// activity-execution Task Queue.

use App\Activities\Basics\ReturnPrimitive\GreetingActivity;
use App\Activities\Execution;
use Temporal\WorkerFactory;

ini_set('display_errors', 'stderr');
require __DIR__ . '/../../vendor/autoload.php';

$factory = WorkerFactory::create();
$worker = $factory->newWorker('activity-execution');
$worker->registerWorkflowTypes(Execution\GreetingWorkflow::class, Execution\MultipleStubsWorkflow::class);
$worker->registerActivity(GreetingActivity::class);
$factory->run();
