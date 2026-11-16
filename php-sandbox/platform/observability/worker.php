<?php
declare(strict_types=1);

// Runs the Workflows from the Observability page:
//  - your-task-queue: default logger, logging in replay enabled
//  - observability-orders: OrderWorkflow with the Search Attribute Updates

use App\Platform\Observability\MyWorkflow;
use App\Platform\Observability\OrderWorkflow;
use App\Platform\Observability\WorkerSetup;

ini_set('display_errors', 'stderr');
require __DIR__ . '/../../vendor/autoload.php';

$setup = new WorkerSetup();
[$factory, $worker] = $setup->loggingInReplay();
$worker->registerWorkflowTypes(MyWorkflow::class);
$factory->newWorker('observability-orders')->registerWorkflowTypes(OrderWorkflow::class);

$factory->run();
