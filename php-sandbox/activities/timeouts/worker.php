<?php
declare(strict_types=1);

// Runs the Workflows and Activities from the Activity Timeouts page on the
// activity-timeouts Task Queue.

use App\Activities\Timeouts;
use Temporal\WorkerFactory;

ini_set('display_errors', 'stderr');
require __DIR__ . '/../../vendor/autoload.php';

$factory = WorkerFactory::create();
$worker = $factory->newWorker('activity-timeouts');
$worker->registerWorkflowTypes(
    Timeouts\TimeoutWorkflow::class,
    Timeouts\RetryPolicyWorkflow::class,
    Timeouts\RetryDelayWorkflow::class,
    Timeouts\DownloadWorkflow::class,
);
$worker->registerActivity(Timeouts\GreetingActivity::class);
$worker->registerActivity(Timeouts\RetryDelayActivity::class);
$worker->registerActivityImplementations(new Timeouts\FileProcessingActivitiesImpl(new Timeouts\Downloader()));
$factory->run();
