<?php
declare(strict_types=1);

// Runs MyWorkflow on my-task-queue with the custom MyLogger.

use App\Platform\Observability\MyWorkflow;
use App\Platform\Observability\WorkerSetup;

ini_set('display_errors', 'stderr');
require __DIR__ . '/../../vendor/autoload.php';

[$factory, $worker] = (new WorkerSetup())->customLogger();
$worker->registerWorkflowTypes(MyWorkflow::class);
$factory->run();
