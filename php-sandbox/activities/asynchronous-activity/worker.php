<?php
declare(strict_types=1);

// Runs the Workflows and Activities from the Asynchronous Activity Completion
// page on the async-activity Task Queue.

use App\Activities\AsynchronousActivity;
use App\Activities\Basics\ReturnPrimitive\GreetingActivity;
use Temporal\WorkerFactory;

ini_set('display_errors', 'stderr');
require __DIR__ . '/../../vendor/autoload.php';

$factory = WorkerFactory::create();
$worker = $factory->newWorker('async-activity');
$worker->registerWorkflowTypes(
    AsynchronousActivity\ParallelGreetingWorkflow::class,
    AsynchronousActivity\AsyncGreetingWorkflow::class,
);
$worker->registerActivity(GreetingActivity::class);
$worker->registerActivity(AsynchronousActivity\GreetingActivity::class);
$factory->run();
