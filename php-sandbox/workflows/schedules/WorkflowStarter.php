<?php
declare(strict_types=1);

namespace App\Workflows\Schedules;

use Carbon\CarbonInterval;
use Temporal\Client\WorkflowClientInterface;
use Temporal\Client\WorkflowOptions;
use Temporal\Exception\Client\WorkflowExecutionAlreadyStartedException;

final class WorkflowStarter
{
    public function startWithDelay(WorkflowClientInterface $workflowClient): void
    {
        // @@@SNIPSTART php-schedules-start-delay
        $workflow = $workflowClient->newWorkflowStub(
            GreeterWorkflowInterface::class,
            WorkflowOptions::new()
                ->withWorkflowStartDelay(CarbonInterval::minutes(10)),
        );
        $workflowClient->start($workflow, 'Hello world!');
        // @@@SNIPEND
    }

    public function startCronWorkflow(WorkflowClientInterface $workflowClient): void
    {
        // @@@SNIPSTART php-schedules-cron
        $workflow = $workflowClient->newWorkflowStub(
            CronWorkflowInterface::class,
            WorkflowOptions::new()
                ->withWorkflowId(CronWorkflowInterface::WORKFLOW_ID)
                ->withCronSchedule('* * * * *')
                // Execution timeout limits total time. Cron will stop executing after this timeout.
                ->withWorkflowExecutionTimeout(CarbonInterval::minutes(10))
                // Run timeout limits duration of a single workflow invocation.
                ->withWorkflowRunTimeout(CarbonInterval::minutes(1))
        );

        echo "Starting CronWorkflow...\n";

        try {
            $run = $workflowClient->start($workflow, 'Antony');
            echo "Started: WorkflowID={$run->getExecution()->getID()}\n";
        } catch (WorkflowExecutionAlreadyStartedException $e) {
            echo "Still running as WorkflowID={$e->getExecution()->getID()}\n";
        }
        // @@@SNIPEND
    }
}
