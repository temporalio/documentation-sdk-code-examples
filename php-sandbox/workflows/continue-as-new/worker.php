<?php
declare(strict_types=1);

// Runs the Workflow from the Continue-As-New page on the continue-as-new Task Queue.

use App\Workflows\ContinueAsNew\MessageHandlerWorkflow;
use Temporal\WorkerFactory;

ini_set('display_errors', 'stderr');
require __DIR__ . '/../../vendor/autoload.php';

$factory = WorkerFactory::create();
$factory->newWorker('continue-as-new')->registerWorkflowTypes(MessageHandlerWorkflow::class);
$factory->run();
