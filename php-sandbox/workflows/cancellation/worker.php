<?php
declare(strict_types=1);

// Runs the Workflow and Activity from the Cancellation page on the cancellation Task Queue.

use App\Workflows\Cancellation;
use Temporal\WorkerFactory;

ini_set('display_errors', 'stderr');
require __DIR__ . '/../../vendor/autoload.php';

$factory = WorkerFactory::create();
$worker = $factory->newWorker('cancellation');
$worker->registerWorkflowTypes(Cancellation\CancelActivityWorkflow::class);
$worker->registerActivity(Cancellation\MyActivity::class);
$factory->run();
