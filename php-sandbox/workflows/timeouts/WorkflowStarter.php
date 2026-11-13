<?php
declare(strict_types=1);

namespace App\Workflows\Timeouts;

use Carbon\CarbonInterval;
use Temporal\Client\WorkflowClientInterface;
use Temporal\Client\WorkflowOptions;
use Temporal\Common\IdReusePolicy;
use Temporal\Common\RetryOptions;

final class WorkflowStarter
{
    public function __construct(
        private readonly WorkflowClientInterface $workflowClient,
    ) {}

    public function newWorkflowWithTimeout(): object
    {
        // @@@SNIPSTART php-workflow-timeouts-execution-timeout
        $workflow = $this->workflowClient->newWorkflowStub(
            MyWorkflowInterface::class,
            WorkflowOptions::new()
                ->withWorkflowId('my-workflow-id')
                ->withWorkflowIdReusePolicy(IdReusePolicy::AllowDuplicate)
                // Set Workflow Timeout duration
                ->withWorkflowExecutionTimeout(CarbonInterval::minutes(2))
                // ->withWorkflowRunTimeout(CarbonInterval::minutes(2))
                // ->withWorkflowTaskTimeout(CarbonInterval::minutes(2))
        );
        // @@@SNIPEND

        return $workflow;
    }

    public function newWorkflowWithRetryPolicy(): object
    {
        // @@@SNIPSTART php-workflow-timeouts-retry-policy
        $workflow = $this->workflowClient->newWorkflowStub(
            MyWorkflowInterface::class,
            WorkflowOptions::new()->withRetryOptions(
                RetryOptions::new()->withInitialInterval(120)
            )
        );
        // @@@SNIPEND

        return $workflow;
    }
}
