<?php
declare(strict_types=1);

// Runs every Workflow from the Workflow message passing page. Workflows that
// share a Workflow Type get their own Task Queue.

use App\Workflows\MessagePassing as MP;
use Temporal\Worker\FeatureFlags;
use Temporal\WorkerFactory;

ini_set('display_errors', 'stderr');
require __DIR__ . '/../../vendor/autoload.php';

// Run Signal and Update handlers before the main Workflow method. See the note
// on the Workflow message passing page.
FeatureFlags::$workflowDeferredHandlerStart = true;

$factory = WorkerFactory::create();

$queues = [
    'mp-define' => [[MP\Define\FileProcessing::class], []],
    'mp-handle-signal' => [[
        MP\HandleSignal\YourWorkflow::class,
        MP\SignalFromWorkflow\SignalSenderWorkflow::class,
        MP\SignalFromWorkflow\UntypedSignalSenderWorkflow::class,
    ], []],
    'mp-handle-query' => [[MP\HandleQuery\YourWorkflow::class], []],
    'mp-query-current-state' => [[MP\QueryCurrentState\YourWorkflow::class], [MP\QueryCurrentState\YourActivity::class]],
    'mp-define-update' => [[MP\DefineUpdate\MyWorkflow::class], []],
    'mp-handle-update' => [[MP\HandleUpdate\FileProcessing::class], []],
    'mp-update-custom-name' => [[MP\UpdateCustomName\FileProcessing::class], []],
    'mp-register-update' => [[MP\RegisterUpdate\PausableWorkflow::class], []],
    'mp-validate-update' => [[MP\ValidateUpdate\Greeting::class], []],
    'mp-update-with-start' => [[MP\UpdateWithStart\ShoppingCart::class], []],
    'mp-wait-in-handler' => [[MP\WaitInHandler\MyWorkflow::class], []],
    'mp-all-handlers-finished' => [[MP\AllHandlersFinished\MyWorkflow::class], []],
    'mp-unfinished-policy' => [[MP\UnfinishedPolicy\MyWorkflow::class], []],
    'mp-workflow-init' => [[MP\WorkflowInit\GreetingExample::class], [MP\WorkflowInit\TitleActivity::class]],
    'mp-handler-concurrency' => [
        [MP\HandlerConcurrency\BadWorkflow::class, MP\HandlerConcurrency\SafeWorkflow::class],
        [MP\HandlerConcurrency\FetchDataActivity::class],
    ],
    'mp-dynamic' => [[MP\Dynamic\DynamicHandlersWorkflow::class], []],
];

foreach ($queues as $taskQueue => [$workflows, $activities]) {
    $worker = $factory->newWorker($taskQueue);
    $worker->registerWorkflowTypes(...$workflows);
    foreach ($activities as $activity) {
        $worker->registerActivity($activity);
    }
}

$factory->run();
