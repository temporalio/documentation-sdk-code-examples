<?php
declare(strict_types=1);

// Runs every Workflow from the Workflow basics page. Workflows that share a
// Workflow Type get their own Task Queue.

use App\Workflows\Basics;
use Temporal\WorkerFactory;

ini_set('display_errors', 'stderr');
require __DIR__ . '/../../vendor/autoload.php';

$factory = WorkerFactory::create();

$workflows = [
    'workflow-basics-define' => Basics\Define\FileProcessing::class,
    'workflow-basics-return-type' => Basics\ReturnValues\FileProcessing::class,
    'workflow-basics-custom-name' => Basics\CustomName\FileProcessing::class,
    'workflow-basics-default-name' => Basics\DefaultName\YourWorkflowDefinition::class,
    'workflow-basics-workflow-init' => Basics\WorkflowInit\GreetingExample::class,
];

foreach ($workflows as $taskQueue => $workflow) {
    $factory->newWorker($taskQueue)->registerWorkflowTypes($workflow);
}

$factory->run();
