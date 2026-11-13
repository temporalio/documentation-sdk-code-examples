<?php
declare(strict_types=1);

// Runs every Workflow from the Child Workflows page on the child-workflows Task Queue.

use App\Workflows\ChildWorkflows;
use Temporal\WorkerFactory;

ini_set('display_errors', 'stderr');
require __DIR__ . '/../../vendor/autoload.php';

$factory = WorkerFactory::create();

$factory->newWorker('child-workflows')->registerWorkflowTypes(
    ChildWorkflows\StubParentWorkflow::class,
    ChildWorkflows\ExecuteParentWorkflow::class,
    ChildWorkflows\AbandonParentWorkflow::class,
    ChildWorkflows\ChildWorkflow::class,
    ChildWorkflows\LongRunningChildWorkflow::class,
);

$factory->run();
