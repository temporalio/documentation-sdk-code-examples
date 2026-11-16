<?php
declare(strict_types=1);

// Runs the Workflows from the Enriching the UI page. Both implement
// YourWorkflow, so each has its own Task Queue.

use App\Platform\EnrichingUi\ActivitySummary;
use App\Platform\EnrichingUi\TimerSummary;
use Temporal\WorkerFactory;

ini_set('display_errors', 'stderr');
require __DIR__ . '/../../vendor/autoload.php';

$factory = WorkerFactory::create();
$factory->newWorker('enriching-ui-timer')->registerWorkflowTypes(TimerSummary\YourWorkflowImpl::class);
$worker = $factory->newWorker('enriching-ui-activity');
$worker->registerWorkflowTypes(ActivitySummary\YourWorkflowImpl::class);
$worker->registerActivity(ActivitySummary\YourActivities::class);
$factory->run();
