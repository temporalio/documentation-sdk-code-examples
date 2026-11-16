<?php
declare(strict_types=1);

// Runs the Activities from the Activity basics page. Each example has its own
// Task Queue because the Greeting examples share an Activity Type.

use App\Activities\Basics;
use Temporal\WorkerFactory;

ini_set('display_errors', 'stderr');
require __DIR__ . '/../../vendor/autoload.php';

$factory = WorkerFactory::create();

$activities = [
    'activity-basics-define' => Basics\Define\FileProcessingActivitiesImpl::class,
    'activity-basics-custom-name' => Basics\CustomName\FileProcessingActivitiesImpl::class,
    'activity-basics-return-primitive' => Basics\ReturnPrimitive\GreetingActivity::class,
    'activity-basics-return-object' => Basics\ReturnObject\GreetingActivity::class,
];

foreach ($activities as $taskQueue => $activity) {
    $worker = $factory->newWorker($taskQueue);
    $worker->registerWorkflowTypes(Basics\ActivityBasicsTestWorkflow::class);
    $worker->registerActivity($activity);
}

$factory->run();
